/********************************************************************************/
/*										*/
/*		BucsTestCaseBubble.java 					*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.bucs;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.batt.BattConstants;
import edu.brown.cs.bubbles.batt.BattConstants.BattNewTestPanel;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;
import edu.brown.cs.bubbles.batt.BattFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussConstants.BussEntry;
import edu.brown.cs.bubbles.buss.BussFactory;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingListPanel;
import edu.brown.cs.ivy.swing.SwingListSet;
import edu.brown.cs.ivy.swing.SwingTextArea;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;




class BucsTestCaseBubble extends BudaBubble implements BucsConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private transient BumpLocation bump_location;
private JPanel		test_panel;
private JButton 	search_button;
private JTextField	keyword_field;
private JComboBox<TestChoice> type_field;
private JButton 	data_button;
private TestChoice	test_type;
private transient TestAction iotest_action;
private transient UserCodePanel user_panel;
private transient BattNewTestPanel iotest_panel;
private transient TestCasePanel case_panel;
private JCheckBox	context_field;
private JLabel		status_field;
private BudaBubble	source_bubble;
private SwingListSet<BucsUserFile> data_files;



public enum TestChoice {
   INPUT_OUTPUT,	// input-output test cases
   USER_TEST,		// let user input test code
   TEST_CASES,		// use existing test cases
}


private static Set<String> bad_words;
private static UserFileType [] file_types = new UserFileType [] {
   UserFileType.READ, UserFileType.WRITE };


static {
   bad_words = new HashSet<String>();
   bad_words.add("the");
   bad_words.add("compute");
   bad_words.add("method");
}

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BucsTestCaseBubble(BumpLocation loc,BudaBubble src)
{
   bump_location = loc;
   source_bubble = src;
   test_type = null;
   iotest_action = new TestAction(loc,BattConstants.NewTestMode.INPUT_OUTPUT);
   user_panel = new UserCodePanel();
   iotest_panel = BattFactory.getFactory().createNewTestPanel(iotest_action);

   String mthd = loc.getSymbolName();
   String prms = loc.getParameters();
   if (prms != null) mthd += prms;
   List<BattTest> tests = BattFactory.getFactory().getAllTestCases(mthd);
   case_panel = null;
   if (tests != null && tests.size() > 0) case_panel = new TestCasePanel(tests);
   JPanel pnl = setupPanel();

   setInteriorColor(BoardColors.getColor("Bucs.TestCaseInterior"));
   setContentPane(pnl);

   loadDataFiles();

   scanForKeywords();
}





/********************************************************************************/
/*										*/
/*	Panel setup								*/
/*										*/
/********************************************************************************/

private JPanel setupPanel()
{
   SwingGridPanel pnl = new TestPanel();
   pnl.setOpaque(false);
   pnl.beginLayout();

   String mthd = bump_location.getSymbolName();
   String ttl = "Code Search for " + mthd;
   pnl.addBannerLabel(ttl);
   SearchListener kl = new SearchListener();
   keyword_field = pnl.addTextField("Keywords",null,kl,kl);
   context_field = pnl.addBoolean("Use Context",true,null);
   data_button = new JButton("Setup Context Data Files");
   data_button.addActionListener(kl);
   pnl.addRawComponent(null,data_button);

   // other options: order by, format style, search engines

   type_field = pnl.addChoice("Input Mode",TestChoice.INPUT_OUTPUT,kl);
   if (case_panel == null) type_field.removeItem(TestChoice.TEST_CASES);

   test_panel = new JPanel(new BorderLayout());
   pnl.addSeparator();
   pnl.addRawComponent("TESTS",test_panel);
   pnl.addSeparator();

   search_button = pnl.addBottomButton("START SEARCH","START SEARCH",kl);
   search_button.setEnabled(false);
   pnl.addBottomButtons();
   pnl.addSeparator();

   status_field = new JLabel();
   status_field.setOpaque(false);
   pnl.addLabellessRawComponent("STATUS",status_field);

   setupTestPanel(TestChoice.INPUT_OUTPUT);

   return pnl;
}




private void setupTestPanel(TestChoice tc)
{
   if (test_panel == null) return;

   JComponent c = null;
   test_type = tc;
   switch (tc) {
      case INPUT_OUTPUT :
	 c = iotest_panel.getPanel();
	 break;
      case USER_TEST :
	 c = user_panel.getComponent();
	 break;
      case TEST_CASES :
	 c = case_panel.getComponent();
	 break;
    }

   if (c != null) {
      test_panel.removeAll();
      test_panel.add(c,BorderLayout.CENTER);
    }

   BudaBubble bb = BudaRoot.findBudaBubble(test_panel);
   if (bb != null) {
      Dimension sz = bb.getPreferredSize();
      System.err.println("PREF SIZE = " + sz + " " + bb.getSize());
      bb.setSize(sz);
    }
}




/********************************************************************************/
/*										*/
/*	Search action								*/
/*										*/
/********************************************************************************/

private void doSearch()
{
   BucsS6Engine eng = new BucsS6Engine(bump_location);

   if (context_field.isSelected() || test_type == TestChoice.TEST_CASES) {
      eng.setDataFiles(data_files.getElements());
      eng.createSearchContext();
    }

   eng.setKeywords(parseKeywords());
   switch (test_type) {
      case INPUT_OUTPUT :
	 eng.setTestCases(iotest_panel.getActiveTests());
	 break;
      case USER_TEST :
	 eng.setTestCode(user_panel.getTestCode());
	 break;
      case TEST_CASES :
	 // eng.setTestCode(case_panel.getTestCode());
	 eng.setUserTest(case_panel.getUserTests());
	 break;
    }

   SearchRequest rq = new SearchRequest();

   eng.startSearch(rq);

   status_field.setText("Doing code search ...");
   BoardColors.setColors(status_field,"Bucs.TestCaseStatus");
   status_field.setOpaque(true);
   Dimension d = getPreferredSize();
   setSize(d);
}


private class SearchRequest implements BucsSearchRequest {

   SearchRequest() { }

   @Override public void handleSearchFailed() {
      status_field.setText("Nothing found from code search");
      BoardColors.setColors(status_field,"Bucs.TestCaseFailed");
    }

   @Override public void handleSearchSucceeded(List<BucsSearchResult> result) {
      status_field.setText("Search completed");
      BoardColors.setColors(status_field,"Bucs.TestCaseSuccess");
      List<BussEntry> sols = new ArrayList<BussEntry>();
      for (BucsSearchResult bsr : result) {
         sols.add(new BucsSearchSolution(bsr,bump_location,source_bubble));
       }
      BudaBubble bb = BussFactory.getFactory().createBubbleStack(sols,450);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BucsTestCaseBubble.this);
      bba.addBubble(bb,BucsTestCaseBubble.this,null,
            BudaConstants.PLACEMENT_LOGICAL|
            BudaConstants.PLACEMENT_GROUPED|BudaConstants.PLACEMENT_NEW);
      BudaBubbleLink bbl = new BudaBubbleLink(BucsTestCaseBubble.this,new BudaDefaultPort(),bb,new BudaDefaultPort());
      bba.addLink(bbl);
    }
   
   @Override public void handleSearchInputs(List<BucsSearchInput> result) {
      
    }

}	// end of inner class SearchRequest



/********************************************************************************/
/*										*/
/*	Internal panel								*/
/*										*/
/********************************************************************************/

private class TestPanel extends SwingGridPanel {

   private static final long serialVersionUID = 1;

   @Override public void paintComponent(Graphics g) {
      g.setColor(getInteriorColor());
      Dimension d = getSize();
      g.fillRect(0, 0, d.width, d.height);
    }

}	// end of inner class TestPanel



/********************************************************************************/
/*										*/
/*	User code panel 							*/
/*										*/
/********************************************************************************/

private class UserCodePanel implements CaretListener {

   private JTextArea	code_area;
   private JScrollPane	scroll_pane;

   UserCodePanel() {
      code_area = new SwingTextArea(5,40);
      code_area.setWrapStyleWord(true);
      code_area.addCaretListener(this);
      scroll_pane = new JScrollPane(code_area);
    }

   JComponent getComponent()		{ return scroll_pane; }

   String getTestCode() {
      return code_area.getText().trim();
    }

   boolean validate() {
      String txt = code_area.getText();
      if (txt.length() < 4) return false;
      return true;
    }

   @Override public void caretUpdate(CaretEvent e) {
      checkStatus();
    }

}	// end of inner class UserCodePanel



/********************************************************************************/
/*										*/
/*	Test case panel 							*/
/*										*/
/********************************************************************************/

private class TestCasePanel implements ListSelectionListener {

   private JList<BattTest> list_component;
   private JScrollPane scroll_pane;

   TestCasePanel(Collection<BattTest> tests) {
      Vector<BattTest> vd = new Vector<BattTest>(tests);
      list_component = new JList<BattTest>(vd);
      list_component.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      list_component.setVisibleRowCount(10);
      list_component.addListSelectionListener(this);
      scroll_pane = new JScrollPane(list_component);
   
      for (int i = 0; i < vd.size(); ++i) {
         BattTest bt = vd.get(i);
         switch (bt.usesMethod(bump_location.getSymbolName())) {
            case DIRECT :
               list_component.addSelectionInterval(i,i);
               break;
            default :
               break;
          }
       }
    }

   JComponent getComponent()		{ return scroll_pane; }

   List<BattTest> getUserTests() {
      List<BattTest> rslt = new ArrayList<BattTest>();
      for (BattTest bt : list_component.getSelectedValuesList()) {
	 rslt.add(bt);
       }
      return rslt;
    }

   boolean validate() {
      List<BattTest> v = list_component.getSelectedValuesList();
      if (v == null || v.size() == 0) return false;

      return true;
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      checkStatus();
    }

}	// end of inner class TestCasePanel



/********************************************************************************/
/*										*/
/*	Automatically find potential keywords					*/
/*										*/
/********************************************************************************/

private void scanForKeywords()
{
   Set<String> words = new HashSet<String>();

   File f = bump_location.getFile();
   String proj = bump_location.getProject();
   BaleConstants.BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(proj,f);
   String code = null;
   try {
      code = bfo.getText(bump_location.getDefinitionOffset(),
	    bump_location.getDefinitionEndOffset() - bump_location.getDefinitionOffset());

    }
   catch (BadLocationException e) { return; }

   boolean inlinecmmt = false;
   boolean inareacmmt = false;
   char lastchar = 0;
   StringBuffer word = null;
   for (int i = 0; i < code.length(); ++i) {
      char ch = code.charAt(i);
      if (inlinecmmt) { 			// check for end of comment
	 if (ch == '\n') {
	    inlinecmmt = false;
	  }
       }
      else if (inareacmmt) {
	 if (lastchar == '*' && ch == '/') {
	    inareacmmt = false;
	    ch = ' ';
	  }
       }
      if (Character.isLetterOrDigit(ch)) {
	 if (inlinecmmt || inareacmmt) {
	    if (word == null) word = new StringBuffer();
	    word.append(Character.toLowerCase(ch));
	  }
	 else break;
       }
      else {
	 if (word != null) {
	    String wd = word.toString();
	    if (!bad_words.contains(wd)) words.add(wd);
	    word = null;
	  }
       }

      if (lastchar == '/') {                    // check for start of comment
	 if (ch == '/') {
	    inlinecmmt = true;
	  }
	 else if (ch == '*') {
	    inareacmmt = true;
	  }
       }
      lastchar = ch;
    }

   if (words.size() > 0) {
      StringBuffer bf = new StringBuffer();
      for (String s : words) {
	 if (!bad_words.contains(s)) {
	    bf.append(s);
	    bf.append(" ");
	 }
       }
      String txt = bf.toString().trim();
      keyword_field.setText(txt);
    }
}



/********************************************************************************/
/*										*/
/*	Handle actions and updates						*/
/*										*/
/********************************************************************************/

private void checkStatus()
{
   boolean sts = true;
   if (keyword_field == null) sts = false;
   else {
      List<String> keys = parseKeywords();
      if (keys == null || keys.size() == 0) sts = false;
    }

   if (type_field != null) {
      TestChoice tc = (TestChoice) type_field.getSelectedItem();
      switch (tc) {
	 case INPUT_OUTPUT :
	    sts &= iotest_panel.validate();
	    break;
	 case USER_TEST :
	    sts &= user_panel.validate();
	    break;
	 case TEST_CASES :
	    sts &= case_panel.validate();
	    break;
       }
    }

   if (search_button != null) search_button.setEnabled(sts);
}




private List<String> parseKeywords()
{
   String cl = keyword_field.getText();
   boolean quotes=false;
   String tmpStr=null;
   boolean add=false;
   boolean backslash=false;
   int i=0;
   List<String> result = new ArrayList<String>();
   quotes=false;
   tmpStr="";
   for (i=0; i < cl.length(); i++) {
      add=true;
      switch (cl.charAt(i)) {
	 case '\\':
	    backslash=true;
	    break;
	 case '"':
	    if (!backslash) {
	       quotes=!quotes;
	       add=false;
	     }
	    backslash=false;
	    break;
	 case ' ':
	    if (!quotes) {
	       result.add(tmpStr.replaceAll("\\\\\"","\""));
	       add=false;
	       tmpStr="";
	     }
	    break;
       }
      if (add) tmpStr+="" + cl.charAt(i);
    }
   if (!tmpStr.equals("")) result.add(tmpStr);
   if (quotes) return null;		// unbalanced quotes

   return result;
}




private class SearchListener implements ActionListener, UndoableEditListener {

   @Override public void actionPerformed(ActionEvent e) {
      if (e.getSource() == type_field) {
         TestChoice tc = (TestChoice) type_field.getSelectedItem();
         if (tc == test_type) return;
         setupTestPanel(tc);
       }
      else if (e.getSource() == search_button) {
         doSearch();
       }
      else if (e.getSource() == data_button) {
         DataFilePanel dfp = new DataFilePanel();
         JOptionPane.showInputDialog(BucsTestCaseBubble.this,dfp);
       }
      else {
         checkStatus();
       }
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      checkStatus();
    }

}	// end of inner class SearchListener



/********************************************************************************/
/*										*/
/*	Data and callback handler for iotest panel				*/
/*										*/
/********************************************************************************/

private class TestAction implements BattConstants.BattTestBubbleCallback {

   private BumpLocation for_location;
   private BattConstants.NewTestMode test_mode;

   TestAction(BumpLocation loc,BattConstants.NewTestMode md) {
      for_location = loc;
      test_mode = md;
    }

   @Override public String getButtonName()			{ return "Start Code Search"; }
   @Override public BumpLocation getLocation()			{ return for_location; }
   @Override public BattConstants.NewTestMode getTestMode()	{ return test_mode; }
   @Override public String getClassName()			{ return null; }
   @Override public boolean getCreateClass()			{ return false; }

   @Override public void itemUpdated() {
      checkStatus();
    }

   @Override public boolean handleTestCases(List<BattConstants.BattCallTest> cts) {
      System.err.println("START SEARCH WITH " + cts);
      return true;
    }

   @Override public void handleTestCases(String code) {
      System.err.println("START SEARCH WITH " + code);
    }

}	// end of inner class TestAction




/********************************************************************************/
/*										*/
/*	Panel for handling data files						*/
/*										*/
/********************************************************************************/

private void loadDataFiles()
{
   data_files = new SwingListSet<BucsUserFile>();

   File f = BoardSetup.getBubblesWorkingDirectory();
   File f1 = new File(f,"bucs.datafiles");
   if (!f1.exists() || f1.length() == 0) return;

   Element xml = IvyXml.loadXmlFromFile(f1);
   for (Element uf : IvyXml.children(xml,"USERFILE")) {
      String nm = IvyXml.getTextElement(uf,"NAME");
      String anm = IvyXml.getTextElement(uf,"JARNAME");
      UserFileType md = IvyXml.getAttrEnum(uf,"ACCESS",UserFileType.READ);
      BucsUserFile buf = new BucsUserFile(new File(nm),anm,md);
      data_files.addElement(buf);
    }
}



private void saveDataFiles()
{
   File f = BoardSetup.getBubblesWorkingDirectory();
   File f1 = new File(f,"bucs.datafiles");
   try {
      IvyXmlWriter xw = new IvyXmlWriter(f1);
      xw.begin("DATAFILES");
      for (BucsUserFile df : data_files) {
	 df.addEntry(xw);
       }
      xw.end("DATAFILES");
      xw.close();
    }
   catch (IOException e) { }
}



private boolean editUserFile(BucsUserFile uf)
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();
   pnl.addBannerLabel("Edit User Data File");
   JTextField lnm = pnl.addFileField("Local File",uf.getFileName(),JFileChooser.FILES_ONLY,null,null);
   JTextField rnm = pnl.addTextField("Remove File (/s6/ or s:)",uf.getAccessName(),null,null);
   int idx = (uf.getFileMode() == UserFileType.READ ? 0 : 1);
   JComboBox<UserFileType> typ = pnl.addChoice("Access",file_types,idx,null);

   int fg = JOptionPane.showOptionDialog(this,pnl,"Edit User Data File",
	 JOptionPane.OK_CANCEL_OPTION,
	 JOptionPane.PLAIN_MESSAGE,
	 null,null,null);
   if (fg != 0) return false;

   String l = lnm.getText();
   String r = rnm.getText();
   UserFileType ft = (UserFileType) typ.getSelectedItem();
   uf.set(new File(l),r,ft);

   return true;
}




private class DataFilePanel extends SwingListPanel<BucsUserFile> {

   private static final long serialVersionUID = 1;

   DataFilePanel() {
      super(data_files);
    }

   @Override protected BucsUserFile createNewItem() {
      BucsUserFile uf = new BucsUserFile();
      if (editUserFile(uf)) {
	 data_files.addElement(uf);
	 saveDataFiles();
       }
      return null;
    }

   @Override protected BucsUserFile editItem(Object itm) {
      BucsUserFile uf = (BucsUserFile) itm;
      if (editUserFile(uf)) saveDataFiles();
      return uf;
    }

   @Override protected BucsUserFile deleteItem(Object itm) {
      BucsUserFile uf = (BucsUserFile) itm;
      data_files.removeElement(uf);
      saveDataFiles();
      return null;
    }

}	// end of inner class DataFilePanel


}	// end of class BucsTestCaseBubble




/* end of BucsTestCaseBubble.java */

