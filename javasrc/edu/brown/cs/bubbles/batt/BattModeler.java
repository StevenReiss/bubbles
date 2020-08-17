/********************************************************************************/
/*										*/
/*		BattModeler.java						*/
/*										*/
/*	Bubble Automated Testing Tool class for maintaining test model in bbls	*/
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


/* SVN: $Id$ */


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;



class BattModeler implements BattConstants, MintHandler
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BattTestCase> test_cases;
private DefaultTableModel	table_model;
private EnumSet<DisplayMode>	display_mode;
private ArrayList<BattTestCase> table_cases;
private SwingEventListenerList<BattModelListener> model_listeners;

private String [] column_names = new String [] {
   "Status", "State", "Class", "Test Name"
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattModeler()
{
   test_cases = new HashMap<>();
   table_model = new DefaultTableModel(column_names,0);
   display_mode = EnumSet.of(DisplayMode.ALL);
   table_cases = new ArrayList<>();
   model_listeners = new SwingEventListenerList<>(BattModelListener.class);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

TableModel getTableModel()		{ return table_model; }

synchronized List<BattTestCase> getAllTests()
{
   return new ArrayList<BattTestCase>(test_cases.values());
}


void addBattModelListener(BattModelListener bl)
{
   model_listeners.add(bl);
}



void removeBattModelListener(BattModelListener bl)
{
   model_listeners.remove(bl);
}




BattTestCase getTestCase(int idx)
{
  if (idx < 0 || idx >= table_cases.size()) return null;
  return table_cases.get(idx);
}



private void modelUpdated()
{
   for (BattModelListener bl : model_listeners) {
      bl.battModelUpdated(this);
    }
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void setDisplayMode(Set<DisplayMode> newmd)
{
   display_mode.clear();
   display_mode.addAll(newmd);
   modelUpdateDisplay();
}



void updateTestModel(Element sts,boolean full)
{
   Set<BattTestCase> del = new HashSet<BattTestCase>();
   if (full) del.addAll(test_cases.values());
   
   synchronized (this) {
      for (Element t : IvyXml.children(sts,"TEST")) {
	 String nm = IvyXml.getAttrString(t,"NAME");
	 boolean newfg = false;
	 BattTestCase btc = test_cases.get(nm);
	 if (btc == null) {
	    btc = new BattTestCase(nm);
	    test_cases.put(nm,btc);
	    newfg = true;
	  }
         del.remove(btc);
	 boolean fg = btc.handleTestState(t);
	 if (newfg) {
	    modelNewTestCase(btc);
	    modelUpdated();
	  }
	 else if (fg) {
	    modelChangeTestCase(btc);
	    modelUpdated();
	  }
       }
      
      for (BattTestCase btc : del) {
         test_cases.remove(btc.getName());
         modelRemoveTestCase(btc);
       }
    }

   modelUpdateDisplay();
}



/********************************************************************************/
/*										*/
/*	Message handling							*/
/*										*/
/********************************************************************************/

@Override public void receive(MintMessage msg,MintArguments args)
{
   String cmd = args.getArgument(0);
   Element e = msg.getXml();

   if (cmd.equals("STATUS")) {
      Updater upd = new Updater(msg,e);
      SwingUtilities.invokeLater(upd);
      return;
    }

   msg.replyTo();
}



private class Updater implements Runnable {
   
   private MintMessage reply_to;
   private Element update_message;
   
   Updater(MintMessage rply,Element xml) {
      reply_to = rply;
      update_message = xml;
    }
   
   @Override public void run() {
      updateTestModel(update_message,false);
      if (reply_to != null) reply_to.replyTo();
    }
}



/********************************************************************************/
/*										*/
/*	Methods to maintain table model 					*/
/*										*/
/********************************************************************************/

private synchronized void modelNewTestCase(BattTestCase btc)
{
   if (!checkDisplay(btc)) return;
   int i = 0;
   while (i < table_cases.size() && btc.compareTo(table_cases.get(i)) > 0) ++i;

   Vector<Object> v = getRowData(btc);

   if (i >= table_cases.size()) {
      table_cases.add(btc);
      table_model.addRow(v);
    }
   else {
      table_cases.add(i,btc);
      table_model.insertRow(i,v);
    }
}



private synchronized void modelChangeTestCase(BattTestCase btc)
{
   int idx = table_cases.indexOf(btc);
   if (idx < 0) return;

   Vector<Object> v = getRowData(btc);
   for (int i = 0; i < v.size(); ++i) {
      if (!v.get(i).equals(table_model.getValueAt(idx,i)))
	 table_model.setValueAt(v.get(i),idx,i);
    }
}


private synchronized void modelRemoveTestCase(BattTestCase btc)
{
   int idx = table_cases.indexOf(btc);
   if (idx < 0) return;
   
   table_cases.remove(btc);
   table_model.removeRow(idx);
}


private synchronized void modelUpdateDisplay()
{
   for (BattTestCase btc : test_cases.values()) {
      int idx = table_cases.indexOf(btc);
      if (checkDisplay(btc)) {
	 if (idx < 0) modelNewTestCase(btc);
       }
      else {
	 if (idx >= 0) {
	    table_cases.remove(idx);
	    table_model.removeRow(idx);
	  }
       }
    }
}




private boolean checkDisplay(BattTestCase btc)
{
   if (display_mode.contains(DisplayMode.FAIL)) {
      if (btc.getStatus() == TestStatus.FAILURE) return true;
    }

   if (display_mode.contains(DisplayMode.NEEDED)) {
      switch (btc.getState()) {
	 case NEEDS_CHECK :
	 case EDITED :
	 case CANT_RUN :
	 case STOPPED :
	    return true;
	 default:
	    break;
       }
    }

   if (display_mode.contains(DisplayMode.PENDING)) {
      switch (btc.getState()) {
	 case PENDING :
	 case RUNNING :
         case TO_BE_RUN :
	 case STOPPED :
	    return true;
	 default:
	    break;
       }
    }

   if (display_mode.contains(DisplayMode.SUCCESS)) {
      if (btc.getStatus() == TestStatus.SUCCESS) return true;
    }

   if (display_mode.contains(DisplayMode.ALL)) {
      if (btc.getState() == TestState.IGNORED) return false;
      return true;
    }

   return false;
}



private Vector<Object> getRowData(BattTestCase btc)
{
   Vector<Object> rslt = new Vector<Object>();
   rslt.add(btc.getStatus());
   rslt.add(btc.getState());
   rslt.add(btc.getClassName());
   rslt.add(btc.getName());

   return rslt;
}




}	// end of class BattModeler




/* end of BattModeler.java */
