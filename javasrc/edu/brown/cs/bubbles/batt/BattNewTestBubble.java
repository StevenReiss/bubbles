/********************************************************************************/
/*										*/
/*		BattNewTestBubble.java						*/
/*										*/
/*	Bubble Automated Testing Tool class for defining new tests		*/
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

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingTextField;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


class BattNewTestBubble implements BattConstants, BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private NewTestMode		test_mode;
private String			method_name;
private BumpLocation		method_data;
private BattTestBubbleCallback	user_callback;
private String			button_name;
private String			in_project;
private String			in_class;
private boolean 		create_class;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattNewTestBubble(String mthd,BumpLocation loc,String inprj,String incls,boolean newcls,NewTestMode md)
{
   test_mode = md;
   method_name = mthd;
   method_data = loc;
   user_callback = null;
   button_name = "Generate";
   in_project = inprj;
   in_class = incls;
   create_class = newcls;
}


BattNewTestBubble(BattTestBubbleCallback cbk)
{
   test_mode = cbk.getTestMode();
   method_data = cbk.getLocation();
   button_name = cbk.getButtonName();
   in_class = cbk.getClassName();
   create_class = cbk.getCreateClass();

   String nm = method_data.getSymbolName();
   int idx = nm.lastIndexOf(".");
   if (idx >= 0) nm = nm.substring(idx+1);
   method_name = nm;
   user_callback = cbk;
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

BudaBubble createNewTestBubble()
{
   if (method_data == null) return null;

   BudaBubble bb = null;

   switch (test_mode) {
      default :
      case USER_CODE :
	 String nm = getTestMethodName();
	 createNewTestMethod(method_name,nm,in_project,in_class,create_class,null);
	 bb = BaleFactory.getFactory().createMethodBubble(in_project,nm);
	 break;
      case INPUT_OUTPUT :
	 bb = new CallMethodBubble(method_name);
	 break;
    }

   return bb;
}


BattNewTestPanel createNewTestPanel()
{
   NewTestArea nta = new NewTestArea(user_callback);
   for (int i = 0; i < 3; ++i) nta.addTestCase();
   return nta;
}


private String getTestMethodName()
{
   String mnm = method_name;
   int idx = mnm.indexOf("(");
   if (idx > 0) mnm = mnm.substring(0,idx);
   idx = mnm.lastIndexOf(".");
   if (idx > 0) mnm = mnm.substring(idx+1);

   BumpClient bc = BumpClient.getBump();

   for (int i = 1; i < 100; ++i) {
      String tnm = "test_" + mnm + "_" + i;
      String fnm = in_class + "." + tnm;
      List<BumpLocation> locs = bc.findMethod(in_project,fnm,false);
      if (locs == null || locs.size() == 0) return fnm;
    }

   return null;
}


/********************************************************************************/
/*										*/
/*	User method handling							*/
/*										*/
/********************************************************************************/

private void createNewTestMethod(String fnm,String nm,String ipnm,String icnm,boolean newcls,String cnts)
{
   int idx = nm.lastIndexOf(".");
   String cnm = nm.substring(0,idx);
   String mnm = nm.substring(idx+1);
   if (cnts == null) cnts = "// insert test code here";
   if (icnm != null) cnm = icnm;

   if (newcls) {
      String xnm = null;
      if (cnm.endsWith("Test")) {
	 xnm = cnm;
	 int ln = xnm.length();
	 xnm = xnm.substring(0,ln-4);
       }
      BuenoProperties props = new BuenoProperties();
      props.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
      if (xnm != null) props.put(BuenoKey.KEY_COMMENT,"Test class for " + xnm);
      else props.put(BuenoKey.KEY_COMMENT,"Test Class");
      props.put(BuenoKey.KEY_NAME,cnm);
      props.put(BuenoKey.KEY_MODIFIERS,Modifier.PUBLIC);
      String cnnm = cnm;
      String pkg = cnnm;
      int idx1 = cnnm.lastIndexOf(".");
      if (idx1 < 0) pkg = null;
      else {
	 pkg = cnnm.substring(0,idx1);
	 cnnm = cnnm.substring(idx1+1);
	 props.put(BuenoKey.KEY_PACKAGE, pkg);
	 props.put(BuenoKey.KEY_NAME, cnnm);
       }
      props.put(BuenoKey.KEY_IMPORTS, "import org.junit.*;");
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(ipnm,cnm,pkg,false);
      BuenoFactory.getFactory().createNew(BuenoType.NEW_CLASS, loc, props);

      loc = BuenoFactory.getFactory().createLocation(ipnm,cnm,null,true);

      props = new BuenoProperties();
      props.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
      props.put(BuenoKey.KEY_COMMENT,"Default Constructor for test class " + cnm);
      String cxnm = cnm;
      int cidx = cxnm.lastIndexOf(".");
      if (cidx > 0) cxnm = cxnm.substring(cidx+1);
      props.put(BuenoKey.KEY_NAME,cxnm);
      props.put(BuenoKey.KEY_MODIFIERS,Modifier.PUBLIC);
      props.put(BuenoKey.KEY_CONTENTS,"\n");

      BuenoFactory.getFactory().createNew(BuenoType.NEW_CONSTRUCTOR,loc,props);
    }

   String anm = null;
   // anm should be last test in class, or null if there are none
   BuenoLocation loc = BuenoFactory.getFactory().createLocation(ipnm,cnm,anm,true);

   BuenoProperties props = new BuenoProperties();
   props.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
   props.put(BuenoKey.KEY_COMMENT,"Test case for " + fnm);
   props.put(BuenoKey.KEY_NAME,mnm);
   props.put(BuenoKey.KEY_RETURNS,"void");
   props.put(BuenoKey.KEY_MODIFIERS,Modifier.PUBLIC);
   props.put(BuenoKey.KEY_CONTENTS,cnts);
   props.put(BuenoKey.KEY_ATTRIBUTES,"@Test");

   BuenoFactory.getFactory().createNew(BuenoType.NEW_METHOD,loc,props);
}



/********************************************************************************/
/*										*/
/*	Input Output method handling						*/
/*										*/
/********************************************************************************/

private class CallMethodBubble extends BudaBubble implements ActionListener, StatusUpdate {

   private NewTestArea test_area;
   private JButton generate_button;
   
   private static final long serialVersionUID = 1;
   
   CallMethodBubble(String fnm) {
      test_area = new NewTestArea(this);
      for (int i = 0; i < 3; ++i) {		   // initial test cases
	 test_area.addTestCase();
       }

      JPanel pnl = new JPanel(new BorderLayout());
      pnl.setOpaque(false);
      pnl.add(test_area.getPanel(),BorderLayout.CENTER);

      JLabel top = new JLabel("Test Cases for " + fnm);
      top.setOpaque(false);

      top.setHorizontalAlignment(SwingConstants.CENTER);
      pnl.add(top,BorderLayout.NORTH);

      generate_button = new JButton(button_name);
      generate_button.addActionListener(this);
      generate_button.setEnabled(false);
      Box bx = Box.createHorizontalBox();
      bx.add(Box.createHorizontalGlue());
      bx.add(generate_button);
      bx.add(Box.createHorizontalGlue());
      pnl.add(bx,BorderLayout.SOUTH);

      setInteriorColor(BoardColors.getColor("Batt.CallMethodInterior")); 

      setContentPane(pnl);
    }

   @Override protected void localDispose() {
      test_area = null;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String mnm = getTestMethodName();
      setVisible(false);
      if (mnm == null) return;
      BattNewTestChecker ckr = new BattNewTestChecker();
      List<BattCallTest> cases = new ArrayList<BattCallTest>(test_area.getActiveTests());
      int sz = cases.size();
      if (sz == 0) return;

      if (user_callback.handleTestCases(cases)) return;

      String rslt = ckr.generateCallTestCode(cases);
      if (rslt == null) return;

      if (user_callback != null) {
	 user_callback.handleTestCases(rslt);
       }
      // create new method
      // bring up bubble on that method
    }

   @Override public void itemUpdated() {
      if (generate_button != null) generate_button.setEnabled(test_area.validate());
    }

}	// end of inner class CallMethodBubble



/********************************************************************************/
/*										*/
/*	NewTestArea -- holder of a set of test cases				*/
/*										*/
/********************************************************************************/

private class NewTestArea implements BattNewTestPanel {

   private SwingGridPanel test_panel;
   private List<NewTestCase> test_cases;
   private StatusUpdate status_update;

   NewTestArea(StatusUpdate upd) {
      status_update = upd;
      test_cases = new ArrayList<NewTestCase>();
      test_panel = new SwingGridPanel();
      test_panel.setOpaque(false);
      test_panel.setInsets(2);
    }

   @Override public JPanel getPanel()		{ return test_panel; }

   int getTestRow(NewTestCase tc)		{ return test_cases.indexOf(tc); }

   void addTestCell(NewTestCase tc,JComponent c,int pos,int span) {
      int r = getTestRow(tc);
      test_panel.addGBComponent(c,pos,r,span,1,1,0);
    }

   void handleUpdate(NewTestCase tc) {
      ensureEmptyTest();
      resetSubsequentTests(tc);
      status_update.itemUpdated();
    }

   void resetSubsequentTests(NewTestCase tc)			{ }

   void ensureEmptyTest() {
      boolean needtest = false;
      int idx = test_cases.size()-1;
      if (idx < 0) needtest = true;
      else if (!test_cases.get(idx).isEmpty()) needtest = true;
   
      if (needtest) {
         addTestCase();
         BudaBubble bb = BudaRoot.findBudaBubble(test_panel);
         Dimension sz = bb.getPreferredSize();
         bb.setSize(sz);
       }
    }

   void addTestCase() {
      NewTestCase ntc = null;
      switch (test_mode) {
         case INPUT_OUTPUT :
            ntc = new CallTestCase(this);
            break;
         case CALL_SEQUENCE :
            ntc = null;
            break;
         case USER_CODE:
            break;
       }
      if (ntc != null) {
         test_cases.add(ntc);
         ntc.setup();
       }
    }

   @Override public boolean validate() {
      BattNewTestChecker bc = new BattNewTestChecker();
      int ntest = 0;
      boolean valid = true;
      for (NewTestCase tc : test_cases) {
         if (tc.isEmpty()) continue;
         if (!tc.validate(bc)) valid = false;
         else ++ntest;
       }
      if (ntest == 0) valid = false;
      return valid;
    }

   @Override public List<BattCallTest> getActiveTests() {
      List<BattCallTest> ltc = new ArrayList<BattCallTest>();
      BattNewTestChecker bc = new BattNewTestChecker();
      for (NewTestCase tc : test_cases) {
         if (!tc.isEmpty() && tc.validate(bc)) ltc.add(tc);
       }
      return ltc;
    }

}	// end of inner class NewTestArea



/********************************************************************************/
/*										*/
/*	NewTestCase -- implementation of a test case				*/
/*										*/
/********************************************************************************/

private abstract class NewTestCase implements BattCallTest, CaretListener, ActionListener, FocusListener {

   protected NewTestArea test_area;
   private String last_error;
   private boolean is_checked;
   protected JTextField test_args;
   protected JTextField test_result;
   protected JComboBox<Enum<?>> test_op;

   NewTestCase(NewTestArea ta) {
      test_area = ta;
      last_error = null;
      is_checked = false;
    }

   @Override public String getTestOutput() {
      if (test_result == null) return null;
      return test_result.getText().trim();
    }

   @Override public String getTestInput() {
      if (test_args == null) return null;
      return test_args.getText().trim();
    }

   @Override public String getTestOp() {
      return test_op.getSelectedItem().toString();
    }
   
   @Override public String getTestOpName() { 
      NewTestOp op = (NewTestOp) (test_op.getSelectedItem());
      return op.name();
   }

   boolean isEmpty() {
      String ta = getTestInput();
      String tb = getTestOutput();
      if (ta != null && !ta.equals("") && !ta.equals("void")) return false;
      if (tb != null && !tb.equals("") && !tb.equals("void")) return false;
      return true;
    }

   abstract void setup();

   void invalidate()				{ is_checked = false; }
   boolean validate(BattNewTestChecker tc) {
      if (!is_checked) {
         last_error = check(tc);
         is_checked = true;
       }
      return last_error == null;
    }
   abstract String check(BattNewTestChecker btc);

   protected JTextField createTextField(int len) {
      JTextField tf = new SwingTextField(len);
      tf.addCaretListener(this);
      tf.addFocusListener(this);
      return tf;
    }

   protected JComboBox<Enum<?>> createSelection(Enum<?> dflt,Enum<?> [] opts) {
      JComboBox<Enum<?>> cbx = new JComboBox<Enum<?>>(opts);
      cbx.setSelectedItem(dflt);
      cbx.addActionListener(this);
      return cbx;
    }

   @Override public void caretUpdate(CaretEvent e) {
      invalidate();
      test_area.handleUpdate(this);
    }

   @Override public void actionPerformed(ActionEvent e) {
      invalidate();
      test_area.handleUpdate(this);
    }

   @Override public void focusGained(FocusEvent e)		{ }
   @Override public void focusLost(FocusEvent e) {
      // validate the test case here
    }

}	// end of inner class NewTestCase



/********************************************************************************/
/*										*/
/*	Call Test case								*/
/*										*/
/********************************************************************************/

private static NewTestOp [] CALL_OPS = { NewTestOp.EQL, NewTestOp.NEQ,
					    NewTestOp.THROW, NewTestOp.SAME,
					    NewTestOp.DIFF, NewTestOp.SHOW };
private static NewTestOp [] VOID_CALL_OPS = { NewTestOp.THROW, NewTestOp.IGNORE };


private class CallTestCase extends NewTestCase {

   private boolean no_return;
   private boolean no_args;
   private String parsed_args;
   private String parsed_result;

   CallTestCase(NewTestArea ta) {
      super(ta);
      String pls = method_data.getParameters();
      no_args = (pls == null || pls.equals("()"));
      pls = method_data.getReturnType();
      no_return = (pls == null || pls.equals("void"));
      parsed_args = null;
      parsed_result = null;
    }
   
   @Override public String getTestInput() {
      if (test_args == null) return null;
      if (parsed_args != null) return parsed_args;
      return test_args.getText().trim();
    }
   
   @Override public String getTestOutput() {
      if (test_result == null) return null;
      if (parsed_result != null) return parsed_result;
      return test_result.getText().trim();
    }

   @Override void setup() {
      Box bx = Box.createHorizontalBox();
      bx.setOpaque(false);
      JLabel l1 = new JLabel("(");
      l1.setOpaque(false);
      bx.add(l1);
      test_args = createTextField(12);
      if (no_args) {
         test_args.setEditable(false);
         test_args.setText("void");
       }
      bx.add(test_args);
      l1 = new JLabel(")");
      l1.setOpaque(false);
      bx.add(l1);
      test_area.addTestCell(this,bx,0,1);
   
      test_result = createTextField(12);
      test_area.addTestCell(this,test_result,2,1);
   
      if (no_return) {
         test_op = createSelection(NewTestOp.IGNORE,VOID_CALL_OPS);
         test_result.setEditable(false);
         test_result.setText("void");
       }
      else {
         test_op = createSelection(NewTestOp.EQL,CALL_OPS);
       }
      test_area.addTestCell(this,test_op,1,1);
    }

   @Override String check(BattNewTestChecker btc) {
      if (test_args == null || test_result == null) return null;
      String tin = test_args.getText().trim();
      String tout = test_result.getText().trim();
      StringBuffer ibuf = new StringBuffer();
      StringBuffer obuf = new StringBuffer();
      parsed_args = null;
      parsed_result = null;
      String err =  btc.checkCallTest(method_data,tin,tout,ibuf,obuf);
      if (err == null) {
         parsed_args = ibuf.toString();
         parsed_result = obuf.toString();
       }
      return err;
    }

}


}	// end of class BattNewTestBubble




/* end of BattNewTestBubble.java */
