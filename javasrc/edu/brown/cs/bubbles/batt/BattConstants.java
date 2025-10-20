/********************************************************************************/
/*										*/
/*		BattConstants.java						*/
/*										*/
/*	Bubble Automated Testing Tool constant definitions			*/
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


import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.jcode.JcodeConstants;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import java.util.Collection;
import java.util.EventListener;
import java.util.List;


public interface BattConstants {


int ASM_API = JcodeConstants.ASM_API;


/********************************************************************************/
/*										*/
/*	File States								*/
/*										*/
/********************************************************************************/

enum FileState {
   STABLE,
   EDITED,
   CHANGED,
   ERRORS;

   public FileState merge(FileState fs) {
      if (fs == null) return this;
      if (fs.ordinal() > ordinal()) return fs;
      return this;
    }

}




/********************************************************************************/
/*										*/
/*	Testing mode								*/
/*										*/
/********************************************************************************/

enum TestMode {
   ON_DEMAND,
   CONTINUOUS
}



/********************************************************************************/
/*										*/
/*	Test Status								*/
/*										*/
/********************************************************************************/

enum TestStatus {
   UNKNOWN,
   SUCCESS,
   FAILURE
}


enum TestState {
   UNKNOWN,
   PENDING,
   TO_BE_RUN,
   RUNNING,
   STOPPED,
   EDITED,
   IGNORED,
   NEEDS_CHECK,
   CANT_RUN,
   UP_TO_DATE
}


enum RunType {
   ALL,
   FAIL,
   PENDING
}



/********************************************************************************/
/*										*/
/*	Menu and button definitions						*/
/*										*/
/********************************************************************************/

String TEST_BUTTON = "Test Management";



enum DisplayMode {
   FAIL,
   NEEDED,
   PENDING,
   SUCCESS,
   ALL
}


interface BattModelListener extends EventListener {

   void battModelUpdated(BattModeler bm);

}


/********************************************************************************/
/*										*/
/*	Test case finding and creation definitions				*/
/*										*/
/********************************************************************************/

enum UseMode {
   NONE,
   UNKNOWN,
   DIRECT,
   INDIRECT
}


enum NewTestMode {
   INPUT_OUTPUT,
   CALL_SEQUENCE,
   USER_CODE
}



/********************************************************************************/
/*										*/
/*	New Test definitions							*/
/*										*/
/********************************************************************************/

enum NewTestOp {

   EQL("=="),
   NEQ("!="),
   SAVE("==>"),
   IGNORE("???"),
   THROW("throws"),
   SAME("==="),
   DIFF("!==="),
   SHOW("< ?? >");

   private String op_text;

   NewTestOp(String t) {
      op_text = t;
    }

   @Override public String toString()		{ return op_text; }

}	// end of enum NewTestOp




enum NewStatus {
   UNCHECKED,
   UNUSED,
   CHECKING,
   BAD,
   OK
}



/********************************************************************************/
/*										*/
/*	External view of a test case						*/
/*										*/
/********************************************************************************/

interface BattTest {

    UseMode usesMethod(String mthd);
    String getName();
    String getClassName();
    String getMethodName();
    TestStatus getStatus();
    TestState getState();
    BattTestCounts getCountData();
    String getFailMessage();
    String getFailTrace();

}


interface BattTestCounts {
   
   Collection<String> getMethods();
   Collection<Integer> getBlocks(String method);
   int getBlockStartLine(String method,int block);
   int getBlockEndLine(String method,int block);
}

interface BattCallTest {

   String getTestInput();
   String getTestOutput();
   String getTestOp();
   String getTestOpName();
   String getTestDescription();

}	// end of interface BattCallTest


/********************************************************************************/
/*										*/
/*	Definitions for new test bubbles					*/
/*										*/
/********************************************************************************/

interface StatusUpdate {
   void itemUpdated();
}

interface BattTestBubbleCallback extends StatusUpdate {

   String getButtonName();
   BumpLocation getLocation();
   NewTestMode getTestMode();
   String getClassName();
   boolean getCreateClass();

   @Override void itemUpdated();
   boolean handleTestCases(List<BattCallTest> tests);
   void handleTestCases(String code);

}

interface BattNewTestPanel {

   JPanel getPanel();
   boolean validate();
   List<BattCallTest> getActiveTests();

}




/********************************************************************************/
/*										*/
/*	Text status display constants						*/
/*										*/
/********************************************************************************/

int BATT_STATUS_COL_MIN_WIDTH = 55;
int BATT_STATUS_COL_MAX_WIDTH = 75;
int BATT_STATE_COL_MIN_WIDTH = 90;
int BATT_STATE_COL_MAX_WIDTH = 110;
int BATT_CLASS_COL_PREF_WIDTH = 150;
int BATT_NAME_COL_PREF_WIDTH = 150;




/********************************************************************************/
/*                                                                              */
/*      Interface for accessing test data                                       */
/*                                                                              */
/********************************************************************************/

interface BattPopupHandler extends EventListener {
   
   void handlePopupMenu(BattTest test,BudaBubble bbl,JPopupMenu menu);
   
}


}	// end of interface BattConstants




/* end of BattConstants.java */
